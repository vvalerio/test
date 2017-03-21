package org.epos.mq;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

public class Manager 
{
	private static Connection connectionToRabbitMQ = null;
	private static ArrayList<Channel> channels;

	public static void init(String sendQueue,String receiveQueue,Handler handler)
	{
		try {
			createConnection();
			declareChannels(receiveQueue);
			createConsumers(sendQueue, receiveQueue, handler);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}	
	}

	private static void createConnection() throws IOException, TimeoutException
	{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("some-rabbit");
		connectionToRabbitMQ = factory.newConnection();
	}

	private static void declareChannels(String receiveQueue) throws IOException 
	{
		channels = new ArrayList<Channel>();
		for(int i = 0; i<10; i++)
		{
			final Channel channel = connectionToRabbitMQ.createChannel();
			channel.queueDeclare(receiveQueue, false, false, false, null);
			channel.basicQos(1);
			channels.add(channel);
		}
		
	};
	
	private static void createConsumers(final String sendQueue,final String receiveQueue,final Handler handler) throws IOException, TimeoutException 
	{
		for(final Channel c : channels)
		{
			Consumer consumer = new DefaultConsumer(c) {
				@Override
				public void handleDelivery(String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
					Runnable thread = new Runnable() {
						public void run() {
							BasicProperties replyProps = new BasicProperties.Builder().correlationId(properties.getCorrelationId()).replyTo(receiveQueue).build();
							System.out.println("[.] Message Received from MessageQueue \nID: "+properties.getCorrelationId());
							try {
								//Handler handler = new Handler();
								byte[] message = handler.handle(new String(body, "UTF-8")).getBytes("UTF-8");
								c.basicPublish("", sendQueue, replyProps, message);
								c.basicAck(envelope.getDeliveryTag(), false);
								
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					thread.run();
				}
			};
				
			c.basicConsume(receiveQueue, false, consumer);
		}
	}
}
