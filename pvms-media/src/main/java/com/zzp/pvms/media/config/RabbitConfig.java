package com.zzp.pvms.media.config;

import com.zzp.pvms.common.constant.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange mediaExchange() {
        return new TopicExchange(MqConstants.MEDIA_EXCHANGE, true, false);
    }

    // 元数据提取队列
    @Bean
    public Queue metadataQueue() {
        return new Queue(MqConstants.METADATA_QUEUE, true);
    }

    @Bean
    public Binding metadataBinding(Queue metadataQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(metadataQueue).to(mediaExchange).with(MqConstants.METADATA_ROUTING_KEY);
    }

    // 缩略图生成队列
    @Bean
    public Queue thumbnailQueue() {
        return new Queue(MqConstants.THUMBNAIL_QUEUE, true);
    }

    @Bean
    public Binding thumbnailBinding(Queue thumbnailQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(thumbnailQueue).to(mediaExchange).with(MqConstants.THUMBNAIL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
