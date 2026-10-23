import com.reelnest.support.CasePolicy;
public final class CasePolicySmoke {
  public static void main(String[] args) {
    assert CasePolicy.allowed("OPEN","IN_REVIEW");
    assert CasePolicy.allowed("IN_REVIEW","RESOLVED");
    assert CasePolicy.allowed("OPEN","REJECTED");
    assert !CasePolicy.allowed("RESOLVED","OPEN");
    assert !CasePolicy.allowed("REJECTED","IN_REVIEW");
    assert !CasePolicy.allowed("OPEN","OPEN");
    assert !CasePolicy.allowed(null,"RESOLVED");
    assert CasePolicy.canAppeal("TECHNICAL","REJECTED",true,false);
    assert CasePolicy.canAppeal("RIGHTS_COMPLAINT","RESOLVED",true,false);
    assert !CasePolicy.canAppeal("APPEAL","REJECTED",true,false);
    assert !CasePolicy.canAppeal("TECHNICAL","OPEN",true,false);
    assert !CasePolicy.canAppeal("TECHNICAL","RESOLVED",false,false);
    assert !CasePolicy.canAppeal("TECHNICAL","RESOLVED",true,true);
    assert CasePolicy.mayUnpublish("RIGHTS_COMPLAINT","IN_REVIEW","RESOLVED",true);
    assert !CasePolicy.mayUnpublish("TECHNICAL","OPEN","RESOLVED",true);
    assert !CasePolicy.mayUnpublish("RIGHTS_COMPLAINT","OPEN","REJECTED",true);
    assert !CasePolicy.mayUnpublish("RIGHTS_COMPLAINT","OPEN","RESOLVED",false);
    System.out.println("CasePolicySmoke: 17 assertions passed");
  }
}
