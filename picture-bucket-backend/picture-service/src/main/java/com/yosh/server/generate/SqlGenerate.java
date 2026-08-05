package com.yosh.server.generate;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.sql.Types;
import java.util.Collections;

@Component
@Profile("generator")
public class SqlGenerate implements CommandLineRunner {

    private final DataSourceProperties dataSourceProperties;

    public SqlGenerate(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    public void run(String... args) {
        Path moduleRoot = moduleRoot();

        FastAutoGenerator.create(
                        dataSourceProperties.getUrl(),
                        dataSourceProperties.getUsername(),
                        dataSourceProperties.getPassword()
                )
                .globalConfig(builder -> builder
                        .author("yosh")
                        .disableOpenDir()
                        .outputDir(moduleRoot.resolve("src/main/java").toString())
                )
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    if (metaInfo.getJdbcType().TYPE_CODE == Types.SMALLINT) {
                        return DbColumnType.INTEGER;
                    }
                    return typeRegistry.getColumnType(metaInfo);
                }))
                .packageConfig(builder -> builder
                        .parent("com.yosh.server")
                        .pathInfo(Collections.singletonMap(
                                OutputFile.xml,
                                moduleRoot.resolve("src/main/resources/mapper").toString()
                        ))
                )
                .strategyConfig(builder -> builder
                        .addInclude("t_simple")
                        .addTablePrefix("t_", "c_")
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private Path moduleRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (workingDirectory.resolve("src/main/java").toFile().isDirectory()) {
            return workingDirectory;
        }
        return workingDirectory.resolve("picture-service");
    }
}
