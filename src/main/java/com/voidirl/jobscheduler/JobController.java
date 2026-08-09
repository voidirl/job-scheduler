package com.voidirl.jobscheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    @GetMapping("/status/{status")
    public List<Job> getJobsByStatus(@PathVariable JobStatus status){
        return jobService.getJobsByStatus(status);
    }
}

