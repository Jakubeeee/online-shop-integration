package com.jakubeeee.tasks.impl.pasttaskexecution;

import com.jakubeeee.common.persistence.AbstractEntityFactory;
import com.jakubeeee.tasks.pasttaskexecution.PastTaskExecutionValue;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Entity factory used for production of {@link PastTaskExecution} from {@link PastTaskExecutionValue} and vice versa.
 */
@NoArgsConstructor(staticName = "getInstance")
public class PastTaskExecutionFactory extends AbstractEntityFactory<PastTaskExecution, PastTaskExecutionValue> {

    @Override
    public PastTaskExecution createEntity(@NonNull PastTaskExecutionValue value) {
        var entity = new PastTaskExecution();
        entity.setTaskId(value.getTaskId());
        entity.setParams(value.getParams());
        entity.setFinishTime(value.getExecutionFinishTime());
        return entity;
    }

    @Override
    public PastTaskExecutionValue createValue(@NonNull PastTaskExecution entity) {
        return new PastTaskExecutionValue(entity.getTaskId(), entity.getParams(), entity.getFinishTime());
    }

}