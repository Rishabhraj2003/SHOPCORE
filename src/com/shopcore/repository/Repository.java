package com.shopcore.repository;

import java.util.List;
import java.util.Optional;

/** Contract every storage class follows. Services depend on this idea, not on files. */
public interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean existsById(ID id);
    void deleteById(ID id);
}
