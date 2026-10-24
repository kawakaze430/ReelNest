"""Build 23 release checks. These do NOT replace a full Spring/Next/Docker run."""
import csv
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[2]
API=ROOT/'apps/api/src/main/java/com/reelnest'
DB=ROOT/'apps/api/src/main/resources/db/migration'
WEB=ROOT/'apps/web'

class Build23Contracts(unittest.TestCase):
 def test_migrations_unique_and_consecutive(self):
  migrations=sorted(DB.glob('V*__*.sql'))
  numbers=sorted(int(re.match(r'V(\d+)__',p.name).group(1)) for p in migrations)
  self.assertEqual(numbers,list(range(1,28)))
  for p in migrations:
   if int(re.match(r'V(\d+)__',p.name).group(1))>=23:
    self.assertIn('REFERENCES',p.read_text())
 def test_watch_party_requires_independent_entitlement(self):
  code=(API/'parties/WatchPartyController.java').read_text()
  for token in ('playback.availability(jwt,profile,title)','@AuthenticationPrincipal Jwt',
                'SELECT profile_id FROM watch_party_members','version=?','PartyPolicy.digest',
                "'ACTIVE' AND expires_at>NOW()"):
   self.assertIn(token,code)
  self.assertNotIn('manifestUrl',code)
  self.assertNotIn('signedCookie',code)
 def test_editorial_public_eligibility(self):
  for path in ('catalog/EditorialCollectionsController.java','catalog/ProgramGuideController.java'):
   src=(API/path).read_text()
   for snippet in ('rt.country_code=?',"r.status='APPROVED'", "m.processing_status='READY'" if path.startswith('catalog/Editorial') else "a.processing_status='READY'", "@PreAuthorize(\"hasRole('ADMIN')\")"):
    self.assertIn(snippet,src)
 def test_offline_service_worker_never_caches_private_routes(self):
  code=(WEB/'public/sw.js').read_text()
  self.assertIn('request.method',code)
  self.assertIn('request.mode',code)
  self.assertIn('/offline.html',code)
  self.assertIn('watch-party',code)
  self.assertIn('account',code)
  self.assertNotIn('cache.put(request',code)
 def test_web_routes_and_localization(self):
  for route in ('collections','channels','watch-party','admin/editorial'):
   self.assertTrue((WEB/'src/app'/route/'page.tsx').is_file())
  self.assertRegex((WEB/'src/components/navbar.tsx').read_text(),
                   r'document\.documentElement\.dir\s*=\s*direction\(')
 def test_no_server_assumed_task_completion(self):
  self.assertTrue((ROOT/'docs/BUILD23_IMPLEMENTATION.md').is_file())
 def test_java_pure_policy_assertions(self):
  if not shutil.which('javac') or not shutil.which('java'):
   self.skipTest('Java compiler not installed')
  with tempfile.TemporaryDirectory() as tmp:
   files=[API/'catalog/EditorialPolicy.java',API/'catalog/ChannelSchedulePolicy.java',API/'parties/PartyPolicy.java']
   sample=Path(tmp)/'CheckPolicies.java'
   sample.write_text('''import com.reelnest.catalog.*;import com.reelnest.parties.*;import java.time.*;
public class CheckPolicies {public static void main(String[] args){
 if(!EditorialPolicy.validSlug("staff-picks")||EditorialPolicy.validSlug("../../evil")||EditorialPolicy.validSlug("with space"))throw new AssertionError();
 Instant now=Instant.parse("2026-01-01T00:00:00Z");
 if(!EditorialPolicy.active("PUBLISHED",now.minusSeconds(2),now.plusSeconds(2),now))throw new AssertionError();
 if(EditorialPolicy.active("ARCHIVED",null,null,now))throw new AssertionError();
 if(!ChannelSchedulePolicy.overlaps(now,now.plusSeconds(100),now.plusSeconds(99),now.plusSeconds(110)))throw new AssertionError();
 if(ChannelSchedulePolicy.overlaps(now,now.plusSeconds(100),now.plusSeconds(100),now.plusSeconds(110)))throw new AssertionError();
 if(ChannelSchedulePolicy.validWindow(now,now.plusSeconds(86401)))throw new AssertionError();
 if(!ChannelSchedulePolicy.validWindow(now,now.plusSeconds(86400)))throw new AssertionError();
 String code=PartyPolicy.invitation();if(code.length()!=48||PartyPolicy.digest(code).length()!=64)throw new AssertionError();
 if(PartyPolicy.digest(code).equals(code))throw new AssertionError();
 if(!PartyPolicy.active("ACTIVE",now.plusSeconds(60),now))throw new AssertionError();
 if(PartyPolicy.active("CLOSED",now.plusSeconds(60),now))throw new AssertionError();
 try{PartyPolicy.boundedPosition(-1);throw new AssertionError();}catch(IllegalArgumentException expected){}
 System.out.println("Policy assertions passed");}}
''')
   subprocess.run(['javac','-d',tmp,*map(str,files),str(sample)],check=True,capture_output=True,text=True,timeout=15)
   subprocess.run(['java','-cp',tmp,'CheckPolicies'],check=True,capture_output=True,text=True,timeout=10)

if __name__=='__main__':unittest.main()
