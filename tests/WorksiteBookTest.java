import java.util.*;
import com.neonshift.app.WorksiteBook;
public class WorksiteBookTest {
 public static void main(String[]args){
  List<String> result=WorksiteBook.merge(Arrays.asList("  12 Main Street  ","12   MAIN STREET",null,"","Day off"),Arrays.asList("12 Main Street","34 South Road"));
  if(!result.equals(Arrays.asList("12 Main Street","34 South Road")))throw new AssertionError(result);
  if(!WorksiteBook.merge(Arrays.asList("34 South Road"),result).equals(Arrays.asList("34 South Road","12 Main Street")))throw new AssertionError("recent order");
  if(WorksiteBook.merge(Arrays.asList(new String(new char[1001]).replace('\0','a')),Collections.emptyList()).size()!=0)throw new AssertionError("length");
  System.out.println("PASS: saved address validation, duplicate detection and recent order.");
 }
}
