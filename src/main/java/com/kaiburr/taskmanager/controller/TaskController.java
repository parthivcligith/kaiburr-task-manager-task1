package com.kaiburr.taskmanager.controller;

import com.kaiburr.taskmanager.model.Task;
import com.kaiburr.taskmanager.model.TaskExecution;
import com.kaiburr.taskmanager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    // GET all tasks
    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // GET task by ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        Optional<Task> task = taskRepository.findById(id);
        return task.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // PUT (create/update) a task
    @PutMapping
    public ResponseEntity<Task> saveTask(@RequestBody Task task) {
        // Simple validation: allow only safe commands (for demo purposes)
        if (task.getCommand().contains("rm") || task.getCommand().contains("shutdown")) {
            return ResponseEntity.badRequest().build();
        }
        Task savedTask = taskRepository.save(task);
        return ResponseEntity.ok(savedTask);
    }

    // DELETE a task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // GET tasks by name
    @GetMapping("/search")
    public ResponseEntity<List<Task>> searchTasksByName(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContainingIgnoreCase(name);
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tasks);
    }

    // PUT task execution (run shell command)
    @PutMapping("/{id}/executions")
    public ResponseEntity<TaskExecution> runTaskCommand(@PathVariable String id) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (!optionalTask.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Task task = optionalTask.get();
    TaskExecution execution = new TaskExecution();
    execution.setStartTime(new Date());

        try {
            // Run shell command
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", task.getCommand());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();

            execution.setOutput(output.toString());
            execution.setEndTime(new Date());

            // Add execution to task
            task.getTaskExecutions().add(execution);
            taskRepository.save(task);

        } catch (Exception e) {
            execution.setOutput("Error executing command: " + e.getMessage());
            execution.setEndTime(new Date());
        }

        return ResponseEntity.ok(execution);
    }
}
