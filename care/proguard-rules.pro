# Firestore maps documents onto the data classes in :shared reflectively, so their
# fields and no-arg constructors must survive shrinking.
-keepclassmembers class com.fangjet.shared.model.** {
    <init>();
    <fields>;
}
