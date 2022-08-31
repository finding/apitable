package com.vikadata.api.modular.social.service.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.vikadata.api.AbstractIntegrationTest;
import com.vikadata.api.enums.social.SocialPlatformType;
import com.vikadata.api.modular.finance.strategy.SocialOrderStrategyFactory;
import com.vikadata.api.modular.space.model.vo.SpaceSubscribeVo;
import com.vikadata.api.util.billing.LarkPlanConfigManager;
import com.vikadata.entity.SocialTenantBindEntity;
import com.vikadata.entity.SpaceEntity;
import com.vikadata.social.feishu.enums.PricePlanType;
import com.vikadata.social.feishu.event.app.OrderPaidEvent;
import com.vikadata.system.config.billing.Price;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 飞书订单服务测试
 */
public class LarkOrderServiceImplTest extends AbstractIntegrationTest {

    @Test
    public void testPriceTenAndOneYear() {
        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_10_1_trail.json");
        assertThat(event).as("数据无法解析:base_10_1_trail").isNotNull();

        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(event.getPricePlanId());
        assertThat(price).as("飞书标准版（10 人）配置错误").isNotNull();
    }

    @Test
    public void testPriceTwentyAndOneYear() {
        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_20_1_upgrade.json");
        assertThat(event).as("数据无法解析:base_20_1_upgrade").isNotNull();

        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(event.getPricePlanId());
        assertThat(price).as("飞书标准版（20 人）配置错误").isNotNull();
    }

    @Test
    public void testPriceThirtyAndOneYear() {
        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_30_1_renew_trail.json");
        assertThat(event).as("数据无法解析:base_30_1_renew_trail").isNotNull();

        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(event.getPricePlanId());
        assertThat(price).as("飞书标准版（30 人）配置错误").isNotNull();
    }

    @Test
    public void testEnterprisePriceThirtyAndOneYear() {
        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/enterprise_30_1_upgrade_after_renew.json");
        assertThat(event).as("数据无法解析:enterprise_30_1_upgrade_after_renew").isNotNull();

        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(event.getPricePlanId());
        assertThat(price).as("飞书企业版（30 人）配置错误").isNotNull();
    }

    @Test
    @Disabled("no assert")
    public void testTrailOrder() {
        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_10_1_trail.json");
        Objects.requireNonNull(event).setPricePlanType(PricePlanType.TRIAL.getType());
        String spaceId = "spc" + IdWorker.get32UUID();
        prepareSocialBindInfo(spaceId, Objects.requireNonNull(event).getTenantKey(), event.getAppId());
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(event);
        // init now time
        final OffsetDateTime nowTime = OffsetDateTime.of(2022, 6, 7, 19, 10, 30, 0, testTimeZone);
        getClock().setTime(nowTime);
        SpaceSubscribeVo vo = iSpaceSubscriptionService.getSpaceSubscription(spaceId);
        assertThat(vo.getOnTrial()).isTrue();
        LocalDateTime startDate =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(event.getPayTime())), testTimeZone);
        LocalDateTime endDate = startDate.plusDays(15);
        assertThat(endDate.toLocalDate()).isEqualTo(vo.getDeadline());
    }

    @Test
    public void testPriceTenAndOneYearOrder() {
        String spaceId = "spc" + IdWorker.get32UUID();
        OrderPaidEvent trailEvent = getOrderPaidEvent("social/feishu/order/base_10_1_trail.json");
        prepareSocialBindInfo(spaceId, Objects.requireNonNull(trailEvent).getTenantKey(), trailEvent.getAppId());
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(trailEvent);

        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_10_1_per_year.json");
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(event);
        SpaceSubscribeVo vo = iSpaceSubscriptionService.getSpaceSubscription(spaceId);
        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(Objects.requireNonNull(event).getPricePlanId());
        assertThat(vo.getOnTrial()).isFalse();
        assertThat(vo.getPlan()).isEqualTo(Objects.requireNonNull(price).getPlanId());
    }

    @Test
    public void testPriceTwentyAndOneYearOrderUpgrade() {
        String spaceId = "spc" + IdWorker.get32UUID();
        OrderPaidEvent trailEvent = getOrderPaidEvent("social/feishu/order/base_10_1_trail.json");
        prepareSocialBindInfo(spaceId, Objects.requireNonNull(trailEvent).getTenantKey(), trailEvent.getAppId());
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(trailEvent);

        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU)
                .retrieveOrderPaidEvent(getOrderPaidEvent("social/feishu/order/base_10_1_per_year.json"));

        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_20_1_upgrade.json");
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(event);
        SpaceSubscribeVo vo = iSpaceSubscriptionService.getSpaceSubscription(spaceId);
        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(Objects.requireNonNull(event).getPricePlanId());
        assertThat(vo.getPlan()).isEqualTo(Objects.requireNonNull(price).getPlanId());
    }

    @Test
    public void testPriceThirtyAndOneYearOrderRenewTrail() {
        String spaceId = "spc" + IdWorker.get32UUID();
        OrderPaidEvent trailEvent = getOrderPaidEvent("social/feishu/order/base_10_1_trail.json");
        prepareSocialBindInfo(spaceId, Objects.requireNonNull(trailEvent).getTenantKey(), trailEvent.getAppId());

        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(trailEvent);

        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU)
                .retrieveOrderPaidEvent(getOrderPaidEvent("social/feishu/order/base_10_1_per_year.json"));

        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/base_20_1_upgrade.json");
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(event);
        SpaceSubscribeVo vo20 = iSpaceSubscriptionService.getSpaceSubscription(spaceId);
        // 续费升级到30人试用, 待生效
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(getOrderPaidEvent(
                "social/feishu/order/base_30_1_renew_trail.json"));
        SpaceSubscribeVo vo = iSpaceSubscriptionService.getSpaceSubscription(spaceId);
        assertThat(vo.getPlan()).isEqualTo(vo20.getPlan());
    }

    @Test
    public void testEnterprisePriceThirtyAndOneYearUpgradeAfterRenew() {
        String spaceId = "spc" + IdWorker.get32UUID();
        OrderPaidEvent trailEvent = getOrderPaidEvent("social/feishu/order/base_10_1_trail.json");
        prepareSocialBindInfo(spaceId, Objects.requireNonNull(trailEvent).getTenantKey(), trailEvent.getAppId());
        // 1. 试用
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(trailEvent);
        // 2. 购买
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU)
                .retrieveOrderPaidEvent(getOrderPaidEvent("social/feishu/order/base_10_1_per_year.json"));
        // 3 升级
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU)
                .retrieveOrderPaidEvent(getOrderPaidEvent("social/feishu/order/base_20_1_upgrade.json"));
        // 4 续费升级到30人试用, 待生效
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(getOrderPaidEvent(
                "social/feishu/order/base_30_1_renew_trail.json"));
        // 5 续费升级之后，再次给原方案升级
        OrderPaidEvent event = getOrderPaidEvent("social/feishu/order/enterprise_30_1_upgrade_after_renew.json");
        SocialOrderStrategyFactory.getService(SocialPlatformType.FEISHU).retrieveOrderPaidEvent(event);
        SpaceSubscribeVo vo = iSpaceSubscriptionService.getSpaceSubscription(spaceId);
        Price price = LarkPlanConfigManager.getPriceByLarkPlanId(Objects.requireNonNull(event).getPricePlanId());
        assertThat(vo.getPlan()).isEqualTo(Objects.requireNonNull(price).getPlanId());
    }


    private OrderPaidEvent getOrderPaidEvent(String filePath) {
        InputStream resourceAsStream = ClassPathResource.class.getClassLoader().getResourceAsStream(filePath);
        if (resourceAsStream == null) {
            return null;
        }
        String jsonString = IoUtil.read(resourceAsStream, StandardCharsets.UTF_8);
        return JSONUtil.toBean(jsonString, OrderPaidEvent.class);
    }

    private void prepareSocialBindInfo(String spaceId, String tenantId, String appId) {
        prepareSpaceData(spaceId);
        SocialTenantBindEntity entity =
                SocialTenantBindEntity.builder().id(IdWorker.getId()).tenantId(tenantId).appId(appId).spaceId(spaceId).build();
        iSocialTenantBindService.save(entity);
    }

    private void prepareSpaceData(String spaceId) {
        // 初始化空间信息
        SpaceEntity spaceEntity = SpaceEntity.builder().spaceId(spaceId).name("测试空间站").build();
        iSpaceService.save(spaceEntity);
    }
}
