"""Build 24: portable regression checks, distinct from Spring integration tests."""
from pathlib import Path
import csv
import re
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'apps/api/src/main/java/com/reelnest'

class ProfilePinContinuationTests(unittest.TestCase):
    def test_missing_pin_method_restored_without_bypassing_auth(self):
        api=(BASE/'profiles/ProfilePinController.java').read_text()
        controller=(BASE/'profiles/ProfileController.java').read_text()
        self.assertRegex(api,r'public void requireForSensitiveAction\(UUID user, UUID profileId, String suppliedPin\)')
        self.assertIn('PinRow row=get(user,profileId)',api)
        self.assertIn('if(!verify(user,profileId,row,suppliedPin))',api)
        self.assertIn('throw new ResponseStatusException(HttpStatus.FORBIDDEN',api)
        self.assertIn('pinController.requireForSensitiveAction(userId,profileId,suppliedPin)',controller)
        self.assertIn('profiles.findByIdAndUserId(profileId, userId)',controller)
        self.assertIn('FOR UPDATE',api)

    def test_failed_pin_attempts_can_commit_when_forbidden(self):
        api=(BASE/'profiles/ProfilePinController.java').read_text()
        controller=(BASE/'profiles/ProfileController.java').read_text()
        self.assertIn('@Transactional(noRollbackFor=ResponseStatusException.class)',controller)
        self.assertGreaterEqual(api.count('@Transactional(noRollbackFor=ResponseStatusException.class)'),2)
        self.assertIn('pin_failed_attempts=?,pin_locked_until=?',api)

    def test_pin_policy_using_real_jdk(self):
        if not shutil.which('javac') or not shutil.which('java'):
            self.skipTest('JDK unavailable')
        with tempfile.TemporaryDirectory(prefix='reelnest-build24-pin-') as directory:
            test=Path(directory)/'VerifyProfilePin.java'
            test.write_text('''import com.reelnest.profiles.ProfilePinPolicy;
import java.time.Instant;
public class VerifyProfilePin {
 public static void main(String[] args){
  byte[] salt=ProfilePinPolicy.salt();
  byte[] hash=ProfilePinPolicy.derive("287594",salt);
  if(!ProfilePinPolicy.matches("287594",salt,hash))throw new AssertionError("correct PIN rejected");
  if(ProfilePinPolicy.matches("287593",salt,hash))throw new AssertionError("wrong PIN accepted");
  if(ProfilePinPolicy.valid("111111")||ProfilePinPolicy.valid("12345")||ProfilePinPolicy.valid("abc123"))throw new AssertionError("weak PIN accepted");
  if(ProfilePinPolicy.nextLock(Instant.EPOCH,4)!=null)throw new AssertionError("early lockout");
  if(!ProfilePinPolicy.locked(Instant.EPOCH,ProfilePinPolicy.nextLock(Instant.EPOCH,5)))throw new AssertionError("lockout disabled");
  if(ProfilePinPolicy.locked(Instant.EPOCH.plusSeconds(61),ProfilePinPolicy.nextLock(Instant.EPOCH,5)))throw new AssertionError("lockout duration");
  System.out.println("ProfilePinPolicy assertions passed");
 }
}''')
            cls=BASE/'profiles/ProfilePinPolicy.java'
            subprocess.run(['javac','-d',directory,str(cls),str(test)],check=True,capture_output=True,text=True,timeout=20)
            run=subprocess.run(['java','-cp',directory,'VerifyProfilePin'],check=True,capture_output=True,text=True,timeout=20)
            self.assertIn('ProfilePinPolicy assertions passed',run.stdout)

    def test_profile_manager_clears_pin_and_invalid_active_selection(self):
        manager=(ROOT/'apps/web/src/components/profile-manager.tsx').read_text()
        self.assertIn("setPinInput(old=>({...old,[id]:''}))",manager)
        self.assertIn("setNewPin(old=>({...old,[id]:''}))",manager)
        self.assertIn("window.dispatchEvent(new Event(PROFILE_EVENT))",manager)
        self.assertIn("window.localStorage.removeItem(selectionKey)",manager)

    def test_canonical_task_ids_unchanged(self):
        with (ROOT/'docs/BUILD24_282_TASKS.csv').open(newline='',encoding='utf-8') as f:
            new=list(csv.DictReader(f))
        with (ROOT/'docs/BUILD23_282_TASKS.csv').open(newline='',encoding='utf-8-sig') as f:
            old=list(csv.DictReader(f))
        self.assertEqual(len(new),282)
        self.assertEqual([(r['task_id'],r['description'],r['build23_status']) for r in old],
                         [(r['task_id'],r['description'],r['build23_status']) for r in new])
        self.assertTrue(all(r['build24_status']==r['build23_status'] for r in new))
        self.assertEqual([r['task_id'] for r in new if r['build24_source_change']=='YES'],['A-13','B-10'])

if __name__=='__main__': unittest.main()
