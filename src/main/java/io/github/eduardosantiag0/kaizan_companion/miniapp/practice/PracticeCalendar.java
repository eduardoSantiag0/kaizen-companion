package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class PracticeCalendar {
    private final Clock clock;

    @Autowired
    public PracticeCalendar(@Value("${kaizan.practice.time-zone:America/Sao_Paulo}") String timeZone) {
        this(Clock.system(ZoneId.of(timeZone)));
    }

    PracticeCalendar(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public String timeZone() {
        return clock.getZone().getId();
    }
}
