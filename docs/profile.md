# Personal profile implementation

Android ports the signed-out `UserProfileActivity` structure from `on-tv-android`: a native top bar,
profile/sign-in card, spaced outlined action sections and the source icon family. The Home avatar
starts this non-exported Activity, and its Back affordance finishes it without rebuilding the
retained Home Fragment.

`UserProfileViewModel` owns the immutable item list and the RecyclerView adapter renders Sign in,
gap and section item types. The available shell contains App settings, About StreamTV, Privacy
policy, Terms and conditions and Feedback. Authentication, settings, policy documents and feedback
destinations do not yet exist in the shared application contract, so selecting them produces an
explicit unavailable message. They can be replaced by real navigation without changing the row
model or Activity layout.

## iOS

`UserProfileView` renders the same signed-out hierarchy from `ProfileCatalog.signedOut`: top bar,
sign-in card with the gradient background, avatar, greeting and Log in button, then the spaced
outlined sections in the same order and with the same converted icon family. Both entry points —
Home's profile affordance and the Short toolbar's — present it as a `fullScreenCover`, so the
destination behind it keeps its state and Back returns to it without a rebuild. Log in and every
action row answer with the same explicit unavailable message until their shared contracts exist.
