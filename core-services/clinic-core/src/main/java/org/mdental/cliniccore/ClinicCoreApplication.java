package org.mdental.cliniccore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "org.mdental.cliniccore.client")
public class ClinicCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicCoreApplication.class, args);
    }

}
