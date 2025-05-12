package com.htc.licenseapproval;

import org.openjdk.jmh.annotations.*;

import com.htc.licenseapproval.dto.CoursesDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;

import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.stream.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)

public class HoursSpentBenchmark {

    private List<RequestResponseDTO> sampleData;

    @Setup(Level.Iteration)
    public void setup() {
        sampleData = IntStream.range(0, 2)
        		.mapToObj(i -> {
                RequestResponseDTO dto = new RequestResponseDTO();
                RequestDetailsDTO detail = new RequestDetailsDTO();
                detail.setCourses(Set.of(new CoursesDTO(1.5f), new CoursesDTO(2.0f)));
                dto.setRequestDetails(Set.of(detail));
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Benchmark
    public float streamVersion() {
        float sum= (float) sampleData.stream()
            .flatMap(req -> req.getRequestDetails().stream())
            .filter(detail -> detail.getCourses() != null)
            .flatMap(detail -> detail.getCourses().stream())
            .mapToDouble(CoursesDTO::getHoursSpent)
            .sum();
 
        return sum;
    }

    @Benchmark
    public float forLoopVersion() {
        float total = 0;
        for (RequestResponseDTO dto : sampleData) {
            for (RequestDetailsDTO detail : dto.getRequestDetails()) {
                if (detail.getCourses() != null) {
                    for (CoursesDTO course : detail.getCourses()) {
                        total += course.getHoursSpent();
                    }
                }
            }
        }

        return total;
    }
}

