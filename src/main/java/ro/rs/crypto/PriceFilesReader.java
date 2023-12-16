package ro.rs.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import ro.rs.crypto.service.CryptoPriceService;

import java.io.File;
import java.nio.file.Paths;

/**
 * Post startup of the application, this class helps to read all the files from the prices dir
 */
@Component
@Slf4j
public class PriceFilesReader implements ApplicationListener<ContextRefreshedEvent> {
    private final CryptoPriceService cryptoPriceService;
    private final ApplicationContext applicationContext;

    public PriceFilesReader(final CryptoPriceService cryptoPriceService,
                                  final ApplicationContext applicationContext) {
        this.cryptoPriceService = cryptoPriceService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            File resource = Paths.get("./prices").toFile();
            if (!resource.exists()) {
                throw new RuntimeException("prices dir not found");
            }

            cryptoPriceService.readAndStoreAllCryptoPrices(resource.toPath());
        } catch (Exception e) {
            throw new RuntimeException("prices dir not found", e);
        }

    }
}
