import com.reelnest.privacy.IdentityTombstone;
public class IdentityTombstoneSmoke {
 public static void main(String[] args) {
  String a=IdentityTombstone.digest("viewer-subject-1");
  String b=IdentityTombstone.digest("viewer-subject-2");
  if(a.length()!=64 || a.equals(b) || !a.equals(IdentityTombstone.digest("viewer-subject-1")))
   throw new AssertionError("Tombstone digest must be stable and subject-specific");
  try {IdentityTombstone.digest(" "); throw new AssertionError("Accepted empty identity");}
  catch(IllegalArgumentException expected) {}
  System.out.println("PASS: SHA-256 erasure tombstone policy");
 }
}
