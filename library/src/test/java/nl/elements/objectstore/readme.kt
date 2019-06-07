package nl.elements.objectstore

import android.content.Context
import android.graphics.Bitmap
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.CryptoConfig
import nl.elements.objectstore.stores.DirectoryStore
import nl.elements.objectstore.stores.PreferencesStore
import nl.elements.objectstore.transformers.ConcealTransformer
import java.util.prefs.Preferences

fun example(store: ObjectStore) {
    if ("id" !in store)
        store["id"] = 123L

    val id: Long = store["id"]

    store.remove("id")
}

fun observe(store: ObjectStore) {
    store
        .toObservable()
        .filter { event -> event.key == "id" }
        .map { event ->
            when (event) {
                is ObjectStore.Event.Updated -> store.get(event.key)
                is ObjectStore.Event.Removed -> -1L
            }
        }
        .subscribe(::println)
}

fun conceal(context: Context) {
    val keyChain = SharedPrefsBackedKeyChain(context, CryptoConfig.KEY_256)
    val crypto = AndroidConceal.get().createDefaultCrypto(keyChain)
    val prefs = context.getSharedPreferences("example", Context.MODE_PRIVATE)

    val store = PreferencesStore(
        preferences = prefs,
        transformer = ConcealTransformer(crypto)
    )
}

fun aggregate(context: Context) {
    // define the stores
    val pictures: ObjectStore = DirectoryStore(context.cacheDir)
    val config: ObjectStore = context
        .getSharedPreferences("config", Context.MODE_PRIVATE)
        .let { PreferencesStore(it) }

    // reduce them into one store
    val stores = listOf(pictures, config)
    val store: ReadableObjectStore = stores.reduce()
}

fun aggregateWithNamespace(context: Context) {
    // define the stores
    val pictures: ObjectStore = DirectoryStore(context.cacheDir)
    val config: ObjectStore = context
        .getSharedPreferences("config", Context.MODE_PRIVATE)
        .let { PreferencesStore(it) }

    // reduce them into one store
    val stores = mapOf("pictures" to pictures, "config" to config)
    val store: ReadableObjectStore = stores.reduceWithNamespace()

    val picture: Bitmap = store["picture:selfie"]
    val token: String = store["config:debug"]
}