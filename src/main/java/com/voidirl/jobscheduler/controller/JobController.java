package com.voidirl.jobscheduler.controller;

import com.voidirl.jobscheduler.dto.JobRequest;
import com.voidirl.jobscheduler.model.Job;
import com.voidirl.jobscheduler.model.JobStatus;
import com.voidirl.jobscheduler.service.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @PostMapping
    public Job createJob(@Valid @RequestBody JobRequest request){
        return jobService.createJob(request);
    }
    @GetMapping
    public List<Job> getAllJobs(){
        return jobService.getAllJobs();
    }
    @GetMapping("/status/{status}")
    public List<Job> getJobsByStatus(@PathVariable JobStatus status){
        return jobService.getJobsByStatus(status);
    }
    @GetMapping("/dead-letter")
    public List<Job> getDeadLetteredJobs(){
        return jobService.getJobsByStatus(JobStatus.DEAD_LETTERED);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id){
        return jobService.getJob(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request){
        return jobService.updateJob(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id){
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}

