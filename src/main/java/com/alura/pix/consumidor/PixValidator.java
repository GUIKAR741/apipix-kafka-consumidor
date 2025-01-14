package com.alura.pix.consumidor;

import com.alura.pix.avro.PixRecord;
import com.alura.pix.dto.PixStatus;
import com.alura.pix.exception.KeyNotFoundException;
import com.alura.pix.model.Key;
import com.alura.pix.model.Pix;
import com.alura.pix.repository.KeyRepository;
import com.alura.pix.repository.PixRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PixValidator {

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private PixRepository pixRepository;

    @KafkaListener(topics = "astin04.poc-pix-topic", groupId = "grupo")
    @RetryableTopic(
            backoff = @Backoff(value = 3000L),
            attempts = "5",
            autoCreateTopics = "true",
            include = KeyNotFoundException.class
    )
    @Transactional
    public void processaPix(PixRecord pixRecord) {
        System.out.println("Pix  recebido: " + pixRecord.getIdentificador());

        Pix pix = pixRepository.findByIdentifier(pixRecord.getIdentificador().toString());

        Key origem = keyRepository.findByChave(pixRecord.getChaveOrigem().toString());
        Key destino = keyRepository.findByChave(pixRecord.getChaveDestino().toString());
        validarPix(pix,origem,destino);
        pixRepository.save(pix);
    }

    private void validarPix(Pix pix, Key origem, Key destino) {
        if (origem == null || destino == null) {
            log.warn("Fail to handle event {}.", pix.getIdentifier());
            pix.setStatus(PixStatus.ERRO);
            throw new KeyNotFoundException();
        } else {
            pix.setStatus(PixStatus.PROCESSADO);
        }
    }

}
