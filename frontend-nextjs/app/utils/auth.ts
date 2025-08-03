// utils/auth.ts
import { signOut, getSession } from "next-auth/react";

export const signOutWithKeycloak = async () => {
  const session = await getSession();

  const logoutUrl = `${process.env.NEXT_LOCAL_KEYCLOAK_URL_RAW}/realms/${process.env.NEXT_PUBLIC_KEYCLOAK_REALM}`+`/connect/endsession?` +
    new URLSearchParams({
      id_token_hint: session?.idToken || "",
      post_logout_redirect_uri: `${process.env.NEXTAUTH_URL}`,
    });

  await signOut({ redirect: false });
  window.location.href = logoutUrl;
};

export default async function federatedLogout() {
  try {
    const response = await fetch("/api/auth/federated-logout");
    const data = await response.json();
    if (response.ok) {
      await signOut({ redirect: false });
      window.location.href = data.url;
      return;
    }
    throw new Error(data.error);
  } catch (error) {
    console.log(error)
    alert(error);
    await signOut({ redirect: false });
    window.location.href = "/";
  }
}
