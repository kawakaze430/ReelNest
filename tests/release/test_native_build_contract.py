"""Validate native compiler inputs/cost guards, not native SDK execution."""
import ast
from pathlib import Path
import re
import xml.etree.ElementTree as ET

import pytest
import yaml


ROOT = Path(__file__).resolve().parents[2]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def workflow():
    return yaml.safe_load((ROOT / '.github/workflows/native-clients.yml').read_text())


def selected_job(job, event, apple_simulators):
    """Evaluate the workflow's actual simple boolean expression over its inputs."""
    expression = workflow()['jobs'][job]['if']
    expression = expression.replace('github.event_name', repr(event))
    expression = expression.replace('inputs.apple_simulators', repr(apple_simulators))
    expression = expression.replace('&&', ' and ').replace('||', ' or ')
    expression = re.sub(r'!(?!=)', ' not ', expression).strip()
    tree = ast.parse(expression, mode='eval')
    allowed = (ast.Expression, ast.BoolOp, ast.And, ast.Or, ast.UnaryOp,
               ast.Not, ast.Compare, ast.Eq, ast.NotEq, ast.Constant)
    assert all(isinstance(node, allowed) for node in ast.walk(tree)), 'Unexpected job-condition syntax'
    return eval(compile(tree, '<native-job-condition>', 'eval'), {'__builtins__': {}})


@pytest.mark.parametrize('event,apple_simulators,android,apple', [
    ('push', False, True, False),
    ('push', True, True, False),
    ('workflow_dispatch', False, True, False),
    ('workflow_dispatch', True, False, True),
])
def test_native_dispatch_selects_only_the_requested_platform(event, apple_simulators, android, apple):
    assert selected_job('android', event, apple_simulators) is android
    assert selected_job('apple', event, apple_simulators) is apple


def test_native_android_pipeline_pins_compatible_toolchain_and_keeps_real_lint():
    native = workflow()
    events = native.get('on', native.get(True))
    assert events['push']['branches'] == ['main']
    assert 'apps/mobile/**' in events['push']['paths']
    job = native['jobs']['android']
    assert job['runs-on'] == 'ubuntu-latest' and job['timeout-minutes'] <= 20
    java = next(step for step in job['steps'] if step.get('uses') == 'actions/setup-java@v4')
    gradle = next(step for step in job['steps'] if step.get('uses') == 'gradle/actions/setup-gradle@v4')
    assert java['with']['java-version'] == '17'
    assert gradle['with']['gradle-version'] == '8.9'
    commands = '\n'.join(step.get('run', '') for step in job['steps'])
    for required in ('platforms;android-35', 'build-tools;34.0.0', ':app:assembleDebug',
                     ':app:assembleRelease', ':app:lintDebug', '-PreelnestApiOrigin=https://api.example.org'):
        assert required in commands
    for weakened in ('--continue', 'abortOnError=false', '-x lint', 'lintOptions'):
        assert weakened not in commands


def test_apple_compilation_is_manual_opt_in_and_never_needs_signing_secrets():
    native = workflow()
    events = native.get('on', native.get(True))
    option = events['workflow_dispatch']['inputs']['apple_simulators']
    assert option['type'] == 'boolean' and option['default'] is False
    job = native['jobs']['apple']
    assert job['if'] == "github.event_name == 'workflow_dispatch' && inputs.apple_simulators"
    assert job['timeout-minutes'] <= 25
    commands = '\n'.join(step.get('run', '') for step in job['steps'])
    for requirement in ('2.44.1', 'shasum -a 256 -c -',
                        'a2e905fb68446e9bb4008cdfe2e13e3f176d0cbcca828b71770f8e53fca91b73',
                        'generic/platform=iOS Simulator', 'generic/platform=tvOS Simulator',
                        'CODE_SIGNING_ALLOWED=NO', 'CODE_SIGNING_REQUIRED=NO',
                        'plistlib.load', "info['CFBundleURLTypes']"):
        assert requirement in commands
    assert 'secrets.' not in str(job)
    assert '-allowProvisioningUpdates' not in commands and 'upload-app' not in commands


def test_android_tv_launcher_has_optional_touchscreen_and_real_landscape_banner():
    base = ROOT / 'apps/mobile/android/app/src/main'
    manifest = ET.parse(base / 'AndroidManifest.xml').getroot()
    features = {item.get(ANDROID + 'name'): item.get(ANDROID + 'required')
                for item in manifest.findall('uses-feature')}
    assert features['android.hardware.touchscreen'] == 'false'
    assert features['android.software.leanback'] == 'false'
    app = manifest.find('application')
    assert app.get(ANDROID + 'banner') == '@drawable/tv_banner'
    assert any(item.get(ANDROID + 'name') == 'android.intent.category.LEANBACK_LAUNCHER'
               for item in app.findall('./activity/intent-filter/category'))
    vector = ET.parse(base / 'res/drawable/tv_banner.xml').getroot()
    assert vector.tag == 'vector'
    assert (vector.get(ANDROID + 'width'), vector.get(ANDROID + 'height')) == ('320dp', '180dp')
    assert float(vector.get(ANDROID + 'viewportWidth')) / float(vector.get(ANDROID + 'viewportHeight')) == 16 / 9
    assert vector.findall('path'), 'A manifest reference alone is not a banner resource'


def test_xcode_targets_use_a_language_mode_and_typed_link_registration():
    project = yaml.safe_load((ROOT / 'apps/mobile/ios/project.yml').read_text())
    expected = {'ReelNestCatalogPreview': 'iOS', 'ReelNestTVPreview': 'tvOS'}
    assert set(project['targets']) == set(expected)
    generated_paths = []
    for name, platform in expected.items():
        target = project['targets'][name]
        assert target['platform'] == platform
        assert target['settings']['base']['SWIFT_VERSION'] == '5.0'
        generated_paths.append(target['info']['path'])
        properties = target['info']['properties']
        assert properties['ReelNestAPIOrigin'] == 'https://api.example.org'
        schemes = properties['CFBundleURLTypes']
        assert isinstance(schemes, list) and len(schemes) == 1
        assert schemes[0]['CFBundleURLSchemes'] == ['reelnest-preview']
        assert not any('INFOPLIST_KEY_CFBundleURLTypes' in key for key in target['settings']['base'])
    assert len(set(generated_paths)) == 2, 'iOS and tvOS must not overwrite each other\'s generated plist'


def test_native_artifacts_are_bounded_and_exclude_signing_key_material():
    native = workflow()
    assert native['permissions'] == {'contents': 'read'}
    for job in native['jobs'].values():
        artifact = next(step for step in job['steps'] if step.get('uses') == 'actions/upload-artifact@v4')
        assert artifact['if'] == 'always()'
        assert artifact['with']['retention-days'] <= 7
        paths = artifact['with']['path']
        for sensitive in ('*.keystore', '*.jks', '*.p12', '*.mobileprovision', '~/', '.gradle/'):
            assert sensitive not in paths
