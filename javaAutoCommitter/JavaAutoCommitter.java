package javaAutoCommitter;

import javax.swing.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

import static javax.swing.JOptionPane.showMessageDialog;

public class JavaAutoCommitter {

    public static String xmlDatabaseName="javaAutoCommitterDatabase.xml";
    public static File veritabaniKlasoru= new File(System.getProperty("user.dir")+"/src/javaAutoCommitter/Database/");
    public static File commitlerinOlduguKlasor= new File(System.getProperty("user.dir")+"/src/javaAutoCommitter/CommitDosyasi/");
    public static HashMap<String, Path> projeler = new HashMap<String, Path>();
    public static File projelerinOlduguKlasor = null;
    public static List<File> islemGormeyenDosyalar = new ArrayList<>();
    public static List<File> islemGorenDosyalar = new ArrayList<>();
    public static List<String> evetHayirSecenekler = Arrays.asList("Evet", "Hayır");

    public static void main(String args[]) throws Exception {

        if(xmlDatabaseName.equals("") || xmlDatabaseName==null || veritabaniKlasoru==null || veritabaniKlasoru.equals(""))
        {
            throw new Exception("Proje bilgilerinin kaydedileceği dosyanın isim değişkeni boştur. İsimlendirme Yapın !");
        }
        if(checkDatabase()==false)
        {
            createDatabase(veritabaniKlasoru+"\\"+xmlDatabaseName);
        }
        else {
            connectDatabase();
        }
        baslangicSoru();
        projeSectir();
        File[] commitlenecekDosyalar = commitlerinOlduguKlasor.listFiles();

        if (commitlenecekDosyalar == null || commitlenecekDosyalar.length == 0) {
            throw new Exception("Commitlenecek Dosya Bulunamadı !");
        }

        List<Map<File, Path>> dosyalarinCommitlenecekYerleri = dosyalarinPathleriniBul(commitlenecekDosyalar);

        for (Map<File, Path> x : dosyalarinCommitlenecekYerleri) {

            Integer cvp = JOptionPane.showConfirmDialog(null, "Bu Dosyayı Commitlemek İstiyor Musunuz ? \n" + x.values() +
                    " : " + x.keySet(), "Seçim Yapınız", 0, 1);
            //cvp YES = 0 NO = 1

            if (cvp == 0) {
                islemGorenDosyalar.add(new File(x.keySet().toString()));
                degisimYap(new File(maptenElemanAl(x.keySet().toString())), new File(maptenElemanAl(x.values().toString())));
            } else {
                islemGormeyenDosyalar.add(new File(x.keySet().toString()));
            }
        }
        listeyiYazdir("İşlem Görmeyen Dosyalar ;", islemGormeyenDosyalar);
        listeyiYazdir("İşlem Gören Dosyalar ;", islemGorenDosyalar);

    }

    public static File klasorSectir() throws Exception {
        JFileChooser f = new JFileChooser();
        File defaultPathFile = new File( System.getProperty("user.dir"));
        f.setCurrentDirectory(defaultPathFile);
        f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.showSaveDialog(null);
        File secilenKlasor = null;
        try {
            secilenKlasor = f.getSelectedFile();
        } catch (Error e) {
            return null;
        }
        if (secilenKlasor == null) {
            return null;
        } else
            return secilenKlasor;
    }


    public static List<Path> dosyaAra(Path path, String fileName) throws IOException {

        List<Path> result;
        try (Stream<Path> pathStream = Files.find(path,
                Integer.MAX_VALUE,
                (p, basicFileAttributes) ->
                        p.getFileName().toString().equalsIgnoreCase(fileName))
        ) {
            result = pathStream.collect(Collectors.toList());
        }
        return result;

    }

    public static void projeSectir() throws Exception {

        Object secilenProje = JOptionPane.showInputDialog(null, "Çalışmak istediğiniz projeyi seçiniz.",
                "Proje Seç.", JOptionPane.DEFAULT_OPTION, null, projeler.keySet().toArray(), "0");

        if (secilenProje != null) {
            projelerinOlduguKlasor=new File(projeler.get(secilenProje).toString());
            File kontrol = new File(commitlerinOlduguKlasor+"/"+secilenProje);
            if (!kontrol.exists())
                kontrol.mkdirs();
            commitlerinOlduguKlasor=new File(commitlerinOlduguKlasor+"/"+secilenProje);

        }
        if (projelerinOlduguKlasor == null) {
            System.err.print("Projelerin Olduğu Klasör Seçilemedi. Bir Sorun Var !.");
            throw new Exception("Projelerin Olduğu Klasör Seçilemedi. Bir Sorun Var !.");
        }
    }

    public static String secimYaptir(List<Object> secenekler, String baslik, String mesaj, String hataMesaji) throws Exception {
        List<String> options = new ArrayList<>();
        for (Object secenek : secenekler) {
            options.add(secenek.toString());
        }

        Object selected = JOptionPane.showInputDialog(null, mesaj,
                baslik, JOptionPane.DEFAULT_OPTION, null, options.toArray(), "0");
        if (selected != null) {
            String selectedString = selected.toString();
            return selectedString;
        } else {
            JOptionPane.showMessageDialog(null, hataMesaji);
            throw new Exception("Seçim Ekranından Beklenemedik Bir Şekilde Çıkıldı Program Sonlandırılıyor !");
        }
    }

    public static List<Map<File, Path>> dosyalarinPathleriniBul(File[] commitlenecekDosyalar) throws Exception {

        List<Map<File, Path>> dosyaYollari = new ArrayList<>();

        for (File islemGorenDosya : commitlenecekDosyalar) {

            List<Path> yollar = dosyaAra(projelerinOlduguKlasor.toPath(), islemGorenDosya.getName());
            Path dosyaProjedekiYolu = null;

            if (yollar.size() == 1) {
                dosyaProjedekiYolu = yollar.get(0);
                if (dosyaProjedekiYolu != null && dosyaProjedekiYolu.isAbsolute()) {
                    Map<File, Path> element = new HashMap<>();
                    element.put(islemGorenDosya, dosyaProjedekiYolu);
                    dosyaYollari.add(element);
                } else
                    throw new Exception(islemGorenDosya.getName() + " --> Bu Dosyanın Dosya Yolunda bir Hata Oluştu");
            } else if (yollar.size() > 1) {
                String tmp = secimYaptir(Arrays.asList(yollar.toArray()), "Değiştirilecek Dosyayı Seçin.", "Birden fazla commitlenecek yer" +
                        " bulundu. Değiştirilecek Dosya yolunu Seçiniz.", "Dosya Seçiminde Bir Hata Meydana Geldi!. Bu dosyada işlem yapılmayacak : " + islemGorenDosya.getName());
                dosyaProjedekiYolu = Paths.get(tmp);
                if (dosyaProjedekiYolu != null && dosyaProjedekiYolu.isAbsolute()) {
                    Map<File, Path> element = new HashMap<>();
                    element.put(islemGorenDosya, dosyaProjedekiYolu);
                    dosyaYollari.add(element);
                } else
                    throw new Exception(islemGorenDosya.getName() + " --> Bu Dosyanın Dosya Yolunda bir Hata Oluştu");
            } else {
                String cvp = secimYaptir(Arrays.asList(evetHayirSecenekler.toArray()), "Seçim Yapınız.", islemGorenDosya.getName() + " Bu Dosya İşlem Görmeyecek Devam Edilsin Mi ?", "Seçimde Bir Hata Meydana Geldi !");

                if (cvp.contains("Evet")) {
                    islemGormeyenDosyalar.add(islemGorenDosya);
                } else if (cvp.contains("Hayır")) {
                    throw new Exception(islemGorenDosya.getName() + " --> Bu Dosyadan Dolayı İşleme devam edilemiyor. Kontrol edip tekrar Deneyiniz !");
                } else
                    throw new Exception("Beklenmedik Bir Hata Oluştu. Evet Hayır Fonksiyonunuzu Kontrol Edin ");
            }

        } // for bitişi

        if (dosyaYollari.size() == 0 || dosyaYollari.isEmpty()) {
            throw new Exception("Hiç Bir Dosyanın Yolu Bulunamadı Programı Tekrar Kontrol Ediniz !");
        } else return dosyaYollari;
    }

    public static void listeyiYazdir(String baslik, List x) {
        System.out.println("\n" + baslik + "\n" + "\n");
        for (Object o : x) {
            System.out.println(o.toString() + "\n");
        }
    }

    public static void degisimYap(File kaynakDosya, File hedefKlasor) throws Exception {
        try {
            Files.copy(kaynakDosya.toPath(), hedefKlasor.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new Exception("Dosya Kopyalanırken Hata Oluştu Program Durduruluyor Kontrol Edin : " + kaynakDosya.getName());
        }

    }

    public static String maptenElemanAl(String x) {
        x = x.substring(1, x.length() - 1);
        return x;
    }

    public static void yeniProjeEkle() throws Exception {
        File proje = klasorSectir();
        if(proje!=null)
        addProjectToDatabase(proje,new File(veritabaniKlasoru+"\\"+xmlDatabaseName));
    }
    public static void baslangicSoru() throws Exception {
        int returnValue=-2;
        while(returnValue!=-1)
        {
            String[] buttons = { "Yeni Proje Ekle","Proje Sil","Başlat"};
            returnValue= JOptionPane.showOptionDialog(null, "Seçim yapınız.", "Proje Ekleme",
                    2, 3, null, buttons, buttons[1]);
            if(returnValue==0)
                yeniProjeEkle();
            if(returnValue==1)
                projeSil();
            if(returnValue==2)
                return;
        }

    }
    public static void projeSil() throws Exception{

        Object secilenProje = JOptionPane.showInputDialog(null, "Silmek istediğiniz projeyi seçiniz.",
                "Proje Seç.", JOptionPane.DEFAULT_OPTION, null, projeler.keySet().toArray(), "0");
        if(secilenProje!=null)
        deleteProjectFromDatabase(secilenProje.toString(),new File(veritabaniKlasoru+"\\"+xmlDatabaseName));
    }
    public static void createDatabase(String yolVeIsim) throws Exception {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element phoneBookRootElement = document.createElement("ProjeYollari");
            document.appendChild(phoneBookRootElement);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(yolVeIsim));
            transformer.transform(domSource, streamResult);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }
    public static Boolean checkDatabase() throws Exception {
        List<Path> tmp =  dosyaAra(Paths.get(System.getProperty("user.dir")+"/src"),xmlDatabaseName);
        if(tmp==null || tmp.size()==0)
        {
            return false;
        }
        else if (tmp.size()>1)
        {
            System.err.println("Birden Fazla Veritabanı bulundu. !");
            for (Path x : tmp) {
                System.err.println(x);
            }
            throw new Exception("Veritabanı ile ilgili bir hata meydana geldi. Program Durduruluyor !");
        }
        else if (tmp.size()==1)
        {
            return true;
        }
        else{
            throw new Exception("Veritabanı ile ilgili bir hata meydana geldi. Program Durduruluyor !");
        }
    }
    public static void connectDatabase() throws Exception {
        List<Path> tmp = dosyaAra(veritabaniKlasoru.toPath(),xmlDatabaseName);
        if(tmp==null || tmp.size()>1 || tmp.size()==0)
        {
            throw new Exception("Veritabanı bağlanırken bir hata ile karşılaşıldı. !");
        }
        File veriTabani = new File(tmp.get(0).toString());

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(veriTabani);

        NodeList projects = document.getElementsByTagName("Project");

        projeler=new HashMap<>();
        for (int i=0; i<=projects.getLength()-1; i++)
        {
            String adi = getElementTagsValue("projeAdi",(Element)projects.item(i));
            String yolu = getElementTagsValue("projeYolu",(Element)projects.item(i));
            projeler.put(adi,Paths.get(yolu));
        }

    }
    public static void deleteProjectFromDatabase(String projectName, File database) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(database);

        NodeList projects = document.getElementsByTagName("Project");

        for (int i=0; i<=projects.getLength()-1; i++)
        {
            String adi = getElementTagsValue("projeAdi",(Element)projects.item(i));
            String yolu = getElementTagsValue("projeYolu",(Element)projects.item(i));

            if(projectName.equals(adi))
            {
                Element x = (Element)projects.item(i);
                x.getParentNode().removeChild(x);
                break;
            }
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(database.toURI()));
        transformer.transform(domSource, streamResult);
        connectDatabase();

    }
    public static void addProjectToDatabase(File project,File database ) throws Exception {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(database);


            Element projeYollariElement = (Element) document.getElementsByTagName("ProjeYollari").item(0);

            Element projeElement = document.createElement("Project");
            projeYollariElement.appendChild(projeElement);
            projeElement.setAttribute("id", String.valueOf(document.getElementsByTagName("projeYolu").getLength()));

            Element projeYoluElement = document.createElement("projeYolu");
            Text projePath = document.createTextNode(project.getPath());
            projeYoluElement.appendChild(projePath);
            projeElement.appendChild(projeYoluElement);

            Element projeAdiElement = document.createElement("projeAdi");
            Text projeAditxt = document.createTextNode(project.getName());
            projeAdiElement.appendChild(projeAditxt);
            projeElement.appendChild(projeAdiElement);


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(database.toURI()));
            transformer.transform(domSource, streamResult);
            connectDatabase();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
    public static String getElementTagsValue(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            NodeList subList = list.item(0).getChildNodes();

            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }

        return null;
    }
}
