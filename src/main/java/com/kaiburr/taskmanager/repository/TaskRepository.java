package com.kaiburr.taskmanager.repository;

import com.kaiburr.taskmanager.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByNameContainingIgnoreCase(String name);
}
