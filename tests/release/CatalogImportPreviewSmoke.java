import com.reelnest.admin.CatalogImportPreview;
public class CatalogImportPreviewSmoke {
 public static void main(String[] a) {
   String id="81a1fac6-8075-4df7-9669-39b2f5b6b0a8";
   var ok=CatalogImportPreview.parse("id,name,genre,language,age_rating\n"+id+",\"A Film, Again\",Drama,hi,UA13\n");
   assert ok.valid() && ok.rows().size()==1;
   assert ok.rows().get(0).name().equals("A Film, Again");
   assert !CatalogImportPreview.parse("id,name,genre,language,age_rating\n"+id+",A,Drama,hi,UA13\n"+id+",B,Drama,hi,U\n").valid();
   assert !CatalogImportPreview.parse("id,name,genre,language,age_rating\n"+id+",A,Drama,hi,INJECT\n").valid();
   assert !CatalogImportPreview.parse("id,name,genre,language,age_rating\n"+id+",A,Drama,hi,U\n"+id+",B,Drama,hi,U\n").valid();
   assert !CatalogImportPreview.parse("id,name,genre,language,age_rating\n"+id+",\"A\"B,Drama,hi,U\n").valid();
   assert !CatalogImportPreview.parse("id,name,genre,language,age_rating\n"+id+",\"Unclosed,Drama,hi,U\n").valid();
   assert CatalogImportPreview.parse("").errors().size()==1;
   System.out.println("CatalogImportPreviewSmoke: 8 assertions passed");
 }
}
