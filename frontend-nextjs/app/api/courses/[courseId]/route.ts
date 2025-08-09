import {NextResponse, NextRequest} from 'next/server';
import {getServerSession} from "next-auth/next";
import {authOptions} from "../../auth/[...nextauth]/route";
import { getToken } from "next-auth/jwt";


// don't cache the results
export const revalidate = 0;

export type ConnectionDetails = {
  serverUrl: string;
  roomName: string;
  participantName: string;
  participantToken: string;
};

export async function GET(
    _req: NextRequest,
    { params }: { params: { courseId: string } }
) {
    const session = await getServerSession(authOptions);

    if (!session) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Si ton backend nécessite le token Keycloak, on le récupère et on le propage
    const token = await getToken({ req: _req }); // nécessite NEXTAUTH_SECRET
    const headers: HeadersInit = {
        "Cache-Control": "no-store",
        ...(token ? { Authorization: `Bearer ${token.accessToken ?? token}` } : {}),
    };

    try {
        const upstream = await fetch(`http://nginx/api/books/${params.courseId}`, {
            headers,
            cache: "no-store",
        });

        if (!upstream.ok) {
            return NextResponse.json(
                { error: "Upstream error", status: upstream.status },
                { status: 502 }
            );
        }

        const data = await upstream.json();
        return NextResponse.json(data, { headers: { "Cache-Control": "no-store" } });
    } catch (err) {
        return NextResponse.json(
            { error: "Fetch failed", details: (err as Error).message },
            { status: 500 }
        );
    }
}
