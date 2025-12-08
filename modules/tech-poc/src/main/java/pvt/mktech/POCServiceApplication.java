package pvt.mktech;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/5 13:20
 *
 * @author Michael
 */
@SpringBootApplication
public class POCServiceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(POCServiceApplication.class, args);

        ElasticsearchService service = context.getBean(ElasticsearchService.class);
        String testEs = "test_es";

        TypeMapping typeMapping = TypeMapping.of(m -> m
                .properties("name", p -> p.text(text -> text.analyzer("standard")))
                .properties("age", p -> p.integer(i -> i))
                 .properties("email", p -> p.keyword(k -> k))
        );
        service.createIndexWithMapping(testEs, typeMapping);
//        Map<String, Object> document = new HashMap<>();
//        document.put("age", 30);
//        document.put("email", "12306@163.com");
//        document.put("name", "赵四");
        User user = new User();
        user.age = 20;
        user.name = "赵四";
        user.email =  "12306@163.com";

        service.createDocument(testEs, "1", user);
        User user2 = service.getDocument(testEs, "1", User.class);
//        List<Map> result = service.searchDocuments(testEs, "", Map.class);
//        System.out.println(result.size());
//        for (Map map : result) {
//            System.out.println(map.get("name"));
//        }
        System.out.println("======" + user2.name);
        user2.name = "li si";
        user2.age = null;

        service.updateDocument(testEs, "1", user2);
        User user3 = service.getDocument(testEs, "1", User.class);
        System.out.println("======" + user3.name);
//        service.deleteDocument(testEs, "1");
//        service.deleteIndex(testEs);


    }


}
class User implements Serializable {

    private static final long serialVersionUID = 4123745572398013766L;
    String name;
    Integer age;
    String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
