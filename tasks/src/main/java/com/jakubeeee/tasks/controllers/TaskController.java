package com.jakubeeee.tasks.controllers;

import com.jakubeeee.tasks.exceptions.InvalidTaskIdException;
import com.jakubeeee.tasks.model.GenericTask;
import com.jakubeeee.tasks.model.LogMessage;
import com.jakubeeee.tasks.model.PastTaskExecution;
import com.jakubeeee.tasks.model.ProgressTracker;
import com.jakubeeee.tasks.publishers.TaskPublisher;
import com.jakubeeee.tasks.service.LoggingService;
import com.jakubeeee.tasks.service.ProgressTrackingService;
import com.jakubeeee.tasks.service.SchedulingService;
import com.jakubeeee.tasks.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TaskController {

    @Autowired
    TaskService taskService;

    @Autowired
    LoggingService loggingService;

    @Autowired
    ProgressTrackingService progressTrackingService;

    @Autowired
    TaskPublisher taskPublisher;

    @Autowired
    SchedulingService schedulingService;

    @GetMapping("tasks")
    public List<GenericTask> getTasks() {
        return taskService.getRegisteredTasks();
    }

    @GetMapping("tasksLogs")
    public List<LogMessage> getTasksLogs() {
        return loggingService.getAllCachedLogs();
    }

    @GetMapping("nextScheduledTasksExecutions")
    public Map<Long, String> getNextScheduledTasksExecutions() {
        return taskService.getNextScheduledTasksExecutions();
    }

    @GetMapping("tasksProgress")
    public Map<Long, ProgressTracker> getTasksProgress() {
        return progressTrackingService.getProgressTrackers();
    }

    @GetMapping("pastTasksExecutions")
    public List<PastTaskExecution> getPastTaskExecutions() {
        return taskService.getPastTaskExecutions();
    }

    @PostMapping(path = "launchTaskManually", consumes = "text/plain")
    public void launchTaskManually(@RequestBody String taskId) throws InvalidTaskIdException {
        schedulingService.launchTaskImmediately(Long.valueOf(taskId));
        taskPublisher.publishTasksProgress(progressTrackingService.getProgressTrackers());
    }

    @PostMapping("clearLogs")
    public void cleaTasksLogs() {
        loggingService.clearLogList();
        taskPublisher.publishAllTasksLogs(loggingService.getAllCachedLogs());
    }

}
