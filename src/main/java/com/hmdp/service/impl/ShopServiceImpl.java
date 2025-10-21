package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_= Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        //缓存击穿
        //Shop shop=queryWithPassThrough(id);
        //-Shop shop=cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY,id, Shop.class,this::getById,RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop=queryWithMutex(id);

        //逻辑过期解决缓存击穿
        //Shop shop=queryWithLogicalExpire(id);
        Shop shop=cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY,id, Shop.class,this::getById,RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);

        if(shop==null){
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

  /*  public Shop queryWithLogicalExpire(Long id){
        //从redis查询商铺缓存
        String key= RedisConstants.CACHE_SHOP_KEY +id;
        String shopJson=stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isBlank(shopJson)) {
            // 存在，直接返回
            return null;
        }
        // 命中，需要把json反序列化为对象
        RedisData redisData=JSONUtil.toBean(shopJson,RedisData.class);
        Shop shop=JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime=redisData.getExpireTime();
        // 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            return shop;
        }
        // 过期，需要缓存重建
        //获取互斥锁
        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
        boolean isLock=tryLock(lockKey);
        //获取锁成功，开启独立线程，实现缓存重建
        if(isLock){
            CACHE_REBUILD_.submit(()-> {
                try{
                    this.saveShop2Redis(id,20L);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
        }

        return shop;
    }

    public Shop queryWithMutex(Long id){
        //从redis查询商铺缓存
        String key= RedisConstants.CACHE_SHOP_KEY +id;
        String shopJson=stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(shopJson)) {
            // 存在，直接返回
            Shop shop= JSONUtil.toBean(shopJson,Shop.class);
            return shop;
        }
        // 判断命中的是否是空值
        if(shopJson!=null){
            return null;
        }

        //获取互斥锁
        String lockKey="lock:shop"+id;
        //判断是否获取成功
        Shop shop= null;
        try {
            boolean isLock=tryLock(lockKey);
            //失败，则休眠并且重启
            if(!isLock){
                Thread.sleep(50);
                return queryWithMutex( id);
            }

            // 成功，根据id查询数据库
            shop = getById(id);
            Thread.sleep(200);
            // 不存在，返回错误
            if(shop==null){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            // 存在，写入redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 释放锁
            unLock(lockKey);
        }

        return shop;
    }

    public Shop queryWithPassThrough(Long id){
        //从redis查询商铺缓存
        String key= RedisConstants.CACHE_SHOP_KEY +id;
        String shopJson=stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(shopJson)) {
            // 存在，直接返回
            Shop shop= JSONUtil.toBean(shopJson,Shop.class);
            return shop;
        }
        // 判断命中的是否是空值
        if(shopJson!=null){
            return null;
        }
        // 不存在，根据id查询数据库
        Shop shop=getById(id);
        // 不存在，返回错误
        if(shop==null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        // 存在，写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }*/

   /* private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }*/

    public void saveShop2Redis(Long id,Long expireSeconds){
        // 查询店铺
        Shop shop=getById(id);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //封装逻辑过期时间
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id=shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        // 更新数据库
        updateById( shop);
        // 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
