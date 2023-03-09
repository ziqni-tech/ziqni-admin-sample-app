package com.ziqni.admin.stores;

import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.watchers.ZiqniSystemCallbackWatcher;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class Stores {

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher;

    private final EventsStore eventsStore;
    private final MemberStore membersStore;
    private final ProductsStore productsStore;
    private final ActionTypesStore actionTypesStore;
    private final AchievementsStore achievementsStore;
    private final CompetitionsStore competitionsStore;
    private final ContestsStore contestsStore;
    private final RewardStore rewardStore;
    private final AwardStore awardStore;
    private final UnitsOfMeasureStore unitsOfMeasureStore;

    private final Set<? extends Store<?,?>> subscribedToCallbacks;

    public Stores(@NonNull ZiqniAdminApiFactory ziqniAdminApiFactory, @NonNull ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;
        this.ziqniSystemCallbackWatcher = ziqniSystemCallbackWatcher;

        this.eventsStore = new EventsStore(ziqniAdminApiFactory);
        this.membersStore = new MemberStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.productsStore = new ProductsStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.actionTypesStore = new ActionTypesStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.achievementsStore = new AchievementsStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.competitionsStore = new CompetitionsStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.contestsStore = new ContestsStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.rewardStore = new RewardStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.awardStore = new AwardStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);
        this.unitsOfMeasureStore = new UnitsOfMeasureStore(ziqniAdminApiFactory, ziqniSystemCallbackWatcher);

        this.subscribedToCallbacks = Set.of(
                this.membersStore,
                this.productsStore,
                this.actionTypesStore,
                this.achievementsStore,
                this.competitionsStore,
                this.contestsStore,
                this.rewardStore,
                this.awardStore,
                this.unitsOfMeasureStore
        );
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    public CompletionStage<Stores> start() {
        return CompletableFuture
                .allOf(
                        this.actionTypesStore.start(),
                        this.unitsOfMeasureStore.start() )
                .orTimeout(5L, TimeUnit.MINUTES)
                .thenApply(unused -> this);
    }

    public void init() {
        this.subscribedToCallbacks.forEach(store ->
                this.ziqniSystemCallbackWatcher.subscribeToEntityChanges(store.getTypeClass())
        );
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

    public CompetitionsStore getCompetitionsStore() {
        return competitionsStore;
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
