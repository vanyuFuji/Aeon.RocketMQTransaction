package org.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.model.VBeTransaction;
import java.util.List;

public interface VBeTransactionMapper {
    List<VBeTransaction> selectAll();
    void markAsProcessed(@Param("id") Long id, @Param("processedId") String processedId);
}