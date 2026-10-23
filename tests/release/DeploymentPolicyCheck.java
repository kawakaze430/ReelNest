import com.reelnest.security.DeploymentPolicy;
public final class DeploymentPolicyCheck {
    static void rejected(Runnable r) {
        try {r.run();throw new AssertionError("Expected fail-closed rejection");}
        catch (IllegalStateException expected) { /* correctly blocked */ }
    }
    public static void main(String[] args) {
        rejected(()->DeploymentPolicy.validate("prod",false,false,"","IN",""));
        rejected(()->DeploymentPolicy.validate("production",true,true,"https://stream.domain.com","IN",""));
        rejected(()->DeploymentPolicy.validate("production",false,false,"https://stream.domain.com","IN",""));
        rejected(()->DeploymentPolicy.validate("production",false,true,"http://stream.domain.com","IN",""));
        rejected(()->DeploymentPolicy.validate("production",false,true,"https://stream.domain.com","IN","http://minio:9000"));
        rejected(()->DeploymentPolicy.validate("production",false,true,"https://stream.domain.com","INVALID",""));
        DeploymentPolicy.validate("development",true,false,"","IN","http://minio:9000");
        DeploymentPolicy.validate("production",false,true,"https://stream.domain.com","IN","");
        System.out.println("PASS: 8 deployment policy assertions");
    }
}
