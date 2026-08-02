package com.lightalm;

import static org.assertj.core.api.Assertions.assertThat;

import com.lightalm.domain.Project;
import com.lightalm.domain.User;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.service.ProjectService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * §8 Phase 3/4/5 — 요구사항/이슈 키 채번의 동시성 안전성 검증(비관적 락).
 * Testcontainers가 필요하므로 이 클래스는 *IT.java로 명명되어 mvn verify(failsafe)에서만 실행된다.
 */
@Testcontainers
@SpringBootTest
class SequenceGenerationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void nextRequirementKey_isUniqueUnderConcurrentAccess() throws Exception {
        User creator = userRepository.save(User.builder()
                .username("concurrency-test")
                .password("hash")
                .email("concurrency-test@example.com")
                .fullName("Concurrency Test")
                .build());
        Project project = projectRepository.save(Project.builder()
                .projectKey("CONC")
                .name("Concurrency Test Project")
                .createdBy(creator)
                .build());

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = java.util.stream.IntStream.range(0, threadCount)
                .<Callable<String>>mapToObj(i -> () -> projectService.nextRequirementKey(project.getId()))
                .toList();

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        Set<String> keys = new java.util.HashSet<>();
        for (Future<String> future : futures) {
            keys.add(future.get());
        }

        assertThat(keys).hasSize(threadCount);
    }
}
