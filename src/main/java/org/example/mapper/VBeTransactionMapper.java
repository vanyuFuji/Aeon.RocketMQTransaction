package org.example.mapper;

import org.example.model.VBeTransaction;
import java.util.List;

public interface VBeTransactionMapper {
    List<VBeTransaction> selectAll();
}