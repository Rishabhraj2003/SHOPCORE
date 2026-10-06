package com.shopcore.repository;

import com.shopcore.config.AppConfig;
import com.shopcore.exception.ShopCoreException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Generic file-backed repository. Data is kept in a ConcurrentHashMap (fast, thread-safe) and the
 * whole map is written to a .dat file after every change. On startup the file is loaded back.
 */
public abstract class FileRepository<T extends Serializable, ID extends Serializable> implements Repository<T, ID> {
    private final Path file;
    private final Function<T, ID> idExtractor;
    protected final Map<ID, T> store = new ConcurrentHashMap<>();
    private final Object persistLock = new Object();

    protected FileRepository(String fileName, Function<T, ID> idExtractor) {
        this.file = AppConfig.dataDir().resolve(fileName);
        this.idExtractor = idExtractor;
        load();
    }

    @Override
    public T save(T entity) {
        store.put(idExtractor.apply(entity), entity);
        persist();
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) { return Optional.ofNullable(store.get(id)); }

    @Override
    public List<T> findAll() { return new ArrayList<>(store.values()); }

    @Override
    public boolean existsById(ID id) { return store.containsKey(id); }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
        persist();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) return;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            store.putAll((Map<ID, T>) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new ShopCoreException("Could not load " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private void persist() {
        synchronized (persistLock) {            // only one thread writes the file at a time
            try {
                Files.createDirectories(file.getParent());
                Path temp = file.resolveSibling(file.getFileName() + ".tmp");
                try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
                    out.writeObject(new HashMap<>(store));
                }
                // write to a temp file first, then swap: a crash can never leave a half-written data file
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ShopCoreException("Could not save " + file.getFileName() + ": " + e.getMessage(), e);
            }
        }
    }
}
