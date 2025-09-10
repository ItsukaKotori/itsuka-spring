package org.itsuka.dict;

import org.itsuka.dict.loader.DictLoader;
import org.springframework.boot.CommandLineRunner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DictCacheLoadRunner implements CommandLineRunner {
    private final DictLoader loader;

    @Override
    public void run(String... args) throws Exception {
        loader.load();
    }
}
