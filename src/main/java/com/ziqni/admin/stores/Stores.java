package com.ziqni.admin.stores;

import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class Stores {

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;

    public final EventsStore eventsStore;
    public final MemberStore membersStore;
    public final ProductsStore productsStore;
    public final ActionTypesStore actionTypesStore;
    public final AchievementsStore achievementsStore;
    public final ContestsStore contestsStore;
    public final RewardStore rewardStore;
    public final AwardStore awardStore;
    public final UnitsOfMeasureStore unitsOfMeasureStore;

    public Stores(@NonNull ZiqniAdminApiFactory ziqniAdminApiFactory) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;
        this.eventsStore = new EventsStore(ziqniAdminApiFactory);
        this.membersStore = new MemberStore(ziqniAdminApiFactory);
        this.productsStore = new ProductsStore(ziqniAdminApiFactory);
        this.actionTypesStore = new ActionTypesStore(ziqniAdminApiFactory);
        this.achievementsStore = new AchievementsStore(ziqniAdminApiFactory);
        this.contestsStore = new ContestsStore(ziqniAdminApiFactory);
        this.rewardStore = new RewardStore(ziqniAdminApiFactory);
        this.awardStore = new AwardStore(ziqniAdminApiFactory);
        this.unitsOfMeasureStore = new UnitsOfMeasureStore(ziqniAdminApiFactory);
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    public CompletionStage<Object> start() {
        return CompletableFuture
                .allOf(
                        this.actionTypesStore.start(),
                        this.unitsOfMeasureStore.start() )
                .orTimeout(5L, TimeUnit.MINUTES)
                .thenApply(unused -> this);
    }

    public EventsStore getEventsStore() {
        return eventsStore;
    }

    public MemberStore getMembersStore() {
        return membersStore;
    }

    public ProductsStore getProductsStore() {
        return productsStore;
    }

    public ActionTypesStore getActionTypesStore() {
        return actionTypesStore;
    }

    public AchievementsStore getAchievementsStore() {
        return achievementsStore;
    }

    public ContestsStore getContestsStore() {
        return contestsStore;
    }

    public RewardStore getRewardStore() {
        return rewardStore;
    }

    public AwardStore getAwardStore() {
        return awardStore;
    }

    public UnitsOfMeasureStore getUnitsOfMeasureStore() {
        return unitsOfMeasureStore;
    }
}
