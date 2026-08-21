package com.voidirl.jobscheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @PostMapping
    public Job createJob(@RequestParam String jobName, @RequestParam String scheduledTime){
        return jobService.createJob(jobName, LocalDateTime.parse(scheduledTime));
    }
    @GetMapping
    public List<Job> getAllJobs(){
        return jobService.getAllJobs();
    }
    @GetMapping("/status/{status}")
    public List<Job> getJobsByStatus(@PathVariable JobStatus status){
        return jobService.getJobsByStatus(status);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id){
        return jobService.getJob(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestParam String jobName, @RequestParam String scheduledTime){
        return jobService.updateJob(id, jobName, LocalDateTime.parse(scheduledTime))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id){
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}

