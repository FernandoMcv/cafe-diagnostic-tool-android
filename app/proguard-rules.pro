# Reglas principales para proteger el código del ofuscador R8/ProGuard

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Proteger Supabase y Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# Proteger PyTorch Lite (Modelos de IA)
-keep class org.pytorch.** { *; }

# Proteger Kotlinx Serialization (DTOs de red)
-keep class com.fcbc.tesis.data.mapper.** { *; }
-keep class kotlinx.serialization.** { *; }