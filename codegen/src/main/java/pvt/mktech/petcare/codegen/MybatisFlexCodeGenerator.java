package pvt.mktech.petcare.codegen;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.config.PackageConfig;
import com.mybatisflex.codegen.config.StrategyConfig;
import com.mybatisflex.codegen.dialect.IDialect;
import com.zaxxer.hikari.HikariDataSource;


public class MybatisFlexCodeGenerator {

    // 数据库配置
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/pet_care_core?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "!QAZ2wsx";
    // 方言
    private static final IDialect DIALECT = IDialect.MYSQL;

    // 项目基础包名
    private static final String BASE_PACKAGE = "pvt.mktech.petcare";

    // 要生成的表名（为空则生成所有表）
    private static final String[] INCLUDED_TABLES = {
            "tb_health_status", "tb_reminder"
    };

    // 要排除的表名
    private static final String[] EXCLUDED_TABLES = {
            "schema_version", "flyway_schema_history"
    };

    public static void main(String[] args) {
        // 创建数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(JDBC_URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);

        // 创建全局配置
        GlobalConfig globalConfig = createGlobalConfig();

        // 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig, DIALECT);

        // 执行生成
        generator.generate();

        System.out.println("代码生成完成！");
    }

    private static GlobalConfig createGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        // 设置生成路径
        globalConfig.setSourceDir("./codegen/src/main/java/generator");

        // 设置包配置
        PackageConfig packageConfig = globalConfig.getPackageConfig();
        packageConfig.setBasePackage(BASE_PACKAGE);
        packageConfig.setEntityPackage(packageConfig.getBasePackage() + ".entity");
        packageConfig.setMapperPackage(packageConfig.getBasePackage() + ".mapper");
        packageConfig.setServicePackage(packageConfig.getBasePackage() + ".service");
        packageConfig.setServiceImplPackage(packageConfig.getBasePackage() + ".service.impl");
        packageConfig.setControllerPackage(packageConfig.getBasePackage() + ".controller");
        packageConfig.setTableDefPackage(packageConfig.getBasePackage() + ".entity.table");

        // 设置策略配置
        StrategyConfig strategyConfig = globalConfig.getStrategyConfig();
        strategyConfig.setGenerateTable(INCLUDED_TABLES);
        strategyConfig.setUnGenerateTable(EXCLUDED_TABLES);
        strategyConfig.setTablePrefix("tb_"); // 设置表前缀

        // 设置列配置
        //可以单独配置某个列
//        ColumnConfig columnConfig = new ColumnConfig();
//        columnConfig.setColumnName("tenant_id");
//        columnConfig.setLarge(true);
//        columnConfig.setVersion(true);
//        globalConfig.getStrategyConfig()
//                .setColumnConfig("tb_account", columnConfig);

        // 生成选项
        globalConfig.setEntityGenerateEnable(true);
        globalConfig.setMapperGenerateEnable(true);
        globalConfig.setServiceGenerateEnable(true);
        globalConfig.setServiceImplGenerateEnable(true);
        globalConfig.setControllerGenerateEnable(true); // 不生成 Controller
        globalConfig.setTableDefGenerateEnable(true);
        globalConfig.setMapperXmlGenerateEnable(false); // 不生成 XML


        // 作者信息
        globalConfig.setAuthor("PetCare Code Generator");

        return globalConfig;
    }
}
