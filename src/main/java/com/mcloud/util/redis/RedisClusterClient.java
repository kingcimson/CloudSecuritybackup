package com.mcloud.util.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RedisClusterClient {

    // 由于在Spring ApplicationContext.xml 配置文件中导入了 redis的配置文件，也就间接的将 <bean id="clusterRedisTemplate"                                                                  class="org.springframework.data.redis.core.RedisTemplate">  这个Bean托管给了Spring bean容器来管理所以 只要我使用注解就可以把这个模板类对象引用过来。
    @Autowired
    private RedisTemplate<String,String> clusterRedisTemplate;

    private static Logger logger = LoggerFactory.getLogger(RedisClusterClient.class);
    public static RedisSerializer<Object> valueRedisSerializer = new JdkSerializationRedisSerializer();
    public static StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();


    /**
     * 设置key和value
     * @param key
     * @param value
     */
    public void set(final String key, final Object value) {
        clusterRedisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                connection.set(stringRedisSerializer.serialize(key), valueRedisSerializer.serialize(value));
                return null;
            }
        });
    }

    /**
     * 获取key值对应的value，如果出现异常返回null
     * @param key
     * @return
     */
    public Object get(final String key) {
        Object object;
        object = clusterRedisTemplate.execute(new RedisCallback<Object>() {
            public Object doInRedis(RedisConnection connection)
                    throws DataAccessException {
                return valueRedisSerializer.deserialize(connection.get(stringRedisSerializer.serialize(key)));
            }
        });
        return object;
    }

    //添加数据
    public void set(Object key, Object value, long liveTime) {
        if(null == value) {
            return;
        }

        if(value instanceof String) {
            if(StringUtils.isEmpty(value.toString())) {
                return;
            }
        }
        final String keyf = key + "";
        final Object valuef = value;
        clusterRedisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] keyb = keyf.getBytes();
                byte[] valueb = toByteArray(valuef);
                connection.set(valueRedisSerializer.serialize(keyb), valueRedisSerializer.serialize(valueb));
                if (liveTime > 0) {
                    connection.expire(valueRedisSerializer.serialize(keyb), liveTime);
                }
                return 1L;
            }
        });
    }

    // 获取数据
    public Object get(Object key) {
        final String keyf = (String) key;
        Object object;
        object = clusterRedisTemplate.execute(new RedisCallback<Object>() {
            public Object doInRedis(RedisConnection connection)
                    throws DataAccessException {

                byte[] key = keyf.getBytes();
                byte[] value = connection.get(key);
                if (value == null) {
                    return null;
                }
                return toObject(value);

            }
        });

        return object;
    }

    /**
     * 描述 : <byte[]转Object>. <br>
     * <p>
     * <使用方法说明>
     * </p>
     *
     * @param bytes
     * @return
     */
    private Object toObject(byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return obj;
    }

    private byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return bytes;
    }

}