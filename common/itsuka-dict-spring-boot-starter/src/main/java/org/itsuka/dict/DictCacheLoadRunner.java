package org.itsuka.dict;

import lombok.RequiredArgsConstructor;
import org.itsuka.dict.loader.DictLoader;
import org.springframework.boot.CommandLineRunner;

@RequiredArgsConstructor
public class DictCacheLoadRunner implements CommandLineRunner {
    private final DictLoader loader;

    @Override
    public void run(String... args) throws Exception {
        loader.load();
    }
}
