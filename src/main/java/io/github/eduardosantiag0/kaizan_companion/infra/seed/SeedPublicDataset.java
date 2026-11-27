package io.github.eduardosantiag0.kaizan_companion.infra.seed;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.ExerciseOrigin;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.GameAnalysisEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.GameAnalysisRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedPublicDataset implements ApplicationRunner {

    private final GameAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (repository.existsByOrigin(ExerciseOrigin.PUBLIC)) {
            log.info("Dataset already imported. Skipping seed.");
            return;
        }
        var resource = new ClassPathResource("dataset/games.json");

        try (var input = resource.getInputStream()) {

            var games = objectMapper
                    .readerForListOf(DatasetGameDto.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .<List<DatasetGameDto>>readValue(input);

            for (var game : games) {
                var entity = GameAnalysisEntity.fromDataset(game);

                repository.save(entity);
            }
        }
    }

}
