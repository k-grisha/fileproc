package gr.fileproc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

public class SerializerTest {

    @Test
    public void asdfgh() {
        Date d = new Date();
        LocalDateTime ld = new java.sql.Timestamp(d.getTime()).toLocalDateTime();
        String s1 = ld.format(DateTimeFormatter.ISO_DATE_TIME);
        System.out.println(SerializationUtils.serialize(s1).length);
        System.out.println(SerializationUtils.serialize(d).length);
        System.out.println(SerializationUtils.serialize(ld).length);

//        DateTimeFormatter.BASIC_ISO_DATE.format(d)
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-AAAAA");

        System.out.println(d);
    }

    @Test
    public void adsfgs() {
        HashMap<String, List<String>> mapa = new HashMap<>();
        mapa.put("k1", List.of("a", "b"));
        var data = SerializationUtils.serialize(mapa);

        var mapa2 = SerializationUtils.deserialize(data);
        System.out.println(mapa2);
    }

    @Test
    public void gfds() {
        Path p = Paths.get("C:\\Hello\\AnotherFolder\\The File Name.PDF");
        String file = p.getFileName().toString();
        System.out.println(file);
    }

    @Test
    public void asdfg() {
        var l1 = List.of("a", "b");
        var l2 = List.of("a", "b");


        System.out.println(l1.equals(l2));

        ArrayList<String> a1 = new ArrayList<>();
        ArrayList<String> a2 = new ArrayList<>();
        a1.add("a");
        a1.add("b");
        a2.add("a");
        a2.add("b");
        System.out.println(a1.equals(a2));


        Map<String, List<String>> m1 = Map.of("k1", l1);
        Map<String, List<String>> m2 = Map.of("k1", l2);
//        m1.put("k1", a1);
//        Map<String, List<String>> m2 = new HashMap<>();
//        m2.put("k1", a2);

        System.out.println(m1.equals(m2));

    }


}
