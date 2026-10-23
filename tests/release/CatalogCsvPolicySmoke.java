import com.reelnest.admin.CatalogCsvPolicy;
public final class CatalogCsvPolicySmoke {
    public static void main(String[] args){
        assert CatalogCsvPolicy.cell("=HYPERLINK(\"x\")").startsWith("\"'=");
        assert CatalogCsvPolicy.cell("  =2+2").startsWith("\"'  =");
        assert CatalogCsvPolicy.cell("\t@evil").startsWith("\"'\t@");
        assert CatalogCsvPolicy.cell("+123").startsWith("\"'+");
        assert CatalogCsvPolicy.cell("-123").startsWith("\"'-");
        assert CatalogCsvPolicy.cell("safe").equals("\"safe\"");
        assert CatalogCsvPolicy.cell("hi\"there").equals("\"hi\"\"there\"");
        assert CatalogCsvPolicy.cell("bad\r\nnew").equals("\"bad  new\"");
        assert CatalogCsvPolicy.cell(null).equals("\"\"");
        System.out.println("CatalogCsvPolicySmoke: 9 assertions passed");
    }
}
