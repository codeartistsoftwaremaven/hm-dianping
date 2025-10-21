package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryTypeList() {
        String key="SHOP_TYPE";
        //从redis中查询
        String shoptype=stringRedisTemplate.opsForValue().get( key);
        //如果不为空，返回缓存中的信息
        if(StrUtil.isNotBlank(shoptype)){
            List<ShopType> shopTypeList= JSONUtil.toList(shoptype, ShopType.class);
            return Result.ok(shopTypeList);
        }
        //如果为空，查询数据库
        List<ShopType> shopTypes=query().orderByAsc("sort").list();
        if(shopTypes.isEmpty()){
            return Result.fail("未找到数据");
        }
        //缓存数据
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}
