package io.ruleforge.solver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"io.ruleforge.solver", "io.ruleforge.common"})
public class SolverServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolverServiceApplication.class, args);
    }
}
