package com.pachoclosystem.pachoclosystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Cliente HTTP hacia MedicamentosService: URL base, timeouts de conexión y de
 * lectura, y la cabecera {@code X-Api-Key} con la clave compartida (si hay).
 */
@Configuration
@EnableConfigurationProperties(MedicamentosProperties.class)
public class MedicamentosClientConfig {

    /** Cabecera con la que MedicamentosService identifica a este servicio. */
    public static final String CABECERA_API_KEY = "X-Api-Key";

    @Bean
    RestClient medicamentosRestClient(MedicamentosProperties propiedades) {
        HttpClient clienteHttp = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(propiedades.timeoutConexion())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteHttp);
        fabrica.setReadTimeout(propiedades.timeoutLectura());
        return configurar(RestClient.builder().requestFactory(fabrica), propiedades).build();
    }

    /**
     * URL base y cabecera de la clave compartida. Separado del transporte para que
     * las pruebas lo apliquen sobre un constructor enlazado a {@code MockRestServiceServer}.
     */
    public static RestClient.Builder configurar(RestClient.Builder constructor, MedicamentosProperties propiedades) {
        constructor.baseUrl(propiedades.url().toString());
        if (propiedades.tieneApiKey()) {
            constructor.defaultHeader(CABECERA_API_KEY, propiedades.apiKey());
        }
        return constructor;
    }
}
