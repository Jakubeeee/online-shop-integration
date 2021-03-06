package com.jakubeeee.tasks.impl.pasttaskexecution;

import com.jakubeeee.tasks.TaskStoreService;
import com.jakubeeee.tasks.impl.TaskPublisher;
import com.jakubeeee.tasks.pasttaskexecution.PastTaskExecutionService;
import com.jakubeeee.tasks.pasttaskexecution.PastTaskExecutionValue;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

import static com.jakubeeee.common.CollectionUtils.filterList;
import static com.jakubeeee.common.DateTimeUtils.getCurrentDateTime;
import static com.jakubeeee.common.DateTimeUtils.isTimeAfter;

/**
 * Default service bean used for operations related to archivization of past task executions.
 */
@RequiredArgsConstructor
@Service
public class DefaultPastTaskExecutionService implements PastTaskExecutionService {

    private static final int PAST_TASKS_EXECUTIONS_REMOVAL_HOURS_THRESHOLD = 72;
    private static final int PAST_TASKS_EXECUTIONS_PER_TASK_MAX_SIZE = 20;

    private static PastTaskExecutionFactory pastTaskExecutionFactory = PastTaskExecutionFactory.getInstance();

    private final TaskStoreService taskStoreService;

    private final TaskPublisher taskPublisher;

    private final PastTaskExecutionRepository pastTaskExecutionRepository;

    @Synchronized
    @Override
    public void registerNewPastTaskExecution(PastTaskExecutionValue pastTaskExecution) {
        pastTaskExecutionRepository.save(pastTaskExecutionFactory.createEntity(pastTaskExecution));
        taskPublisher.publishPastTaskExecutions(getPastTaskExecutions());
    }

    @Override
    public List<PastTaskExecutionValue> getPastTaskExecutions() {
        return pastTaskExecutionFactory.createValues(pastTaskExecutionRepository.findAll());
    }

    @Synchronized
    @Transactional
    @Scheduled(fixedRate = 30000)
    @Override
    public void removeUnnecessaryPastTasksExecutions() {
        LocalDateTime timeHoursAgo = getCurrentDateTime().minusHours(PAST_TASKS_EXECUTIONS_REMOVAL_HOURS_THRESHOLD);
        List<PastTaskExecutionValue> allPastTasksExecutions = getPastTaskExecutions();
        Predicate<PastTaskExecutionValue> obsoletePredicate =
                pastTaskExecution -> isTimeAfter(timeHoursAgo, pastTaskExecution.getExecutionFinishTime());
        pastTaskExecutionFactory.createEntities(
                filterList(allPastTasksExecutions, obsoletePredicate)).forEach(pastTaskExecutionRepository::delete);
        for (var storedTask : taskStoreService.getStoredTasks()) {
            List<PastTaskExecutionValue> pastTasksExecutionsForSingleTask = filterList(
                    filterList(allPastTasksExecutions, obsoletePredicate.negate()),
                    pastTaskExecution -> pastTaskExecution.getTaskId() == storedTask.getId());
            if (pastTasksExecutionsForSingleTask.size() > PAST_TASKS_EXECUTIONS_PER_TASK_MAX_SIZE) {
                int excess = pastTasksExecutionsForSingleTask.size() - PAST_TASKS_EXECUTIONS_PER_TASK_MAX_SIZE;
                List<PastTaskExecutionValue> tempList = pastTasksExecutionsForSingleTask.subList(0, excess);
                pastTaskExecutionFactory.createEntities(tempList).forEach(pastTaskExecutionRepository::delete);
            }
        }
        taskPublisher.publishPastTaskExecutions(getPastTaskExecutions());
    }

}
