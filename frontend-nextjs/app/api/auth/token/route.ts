// Exemple dans un API Route ou middleware
import { getToken } from "next-auth/jwt";
import { NextResponse } from 'next/server';


export async function GET(req, res) {
  const token = await getToken({ req, secret: process.env.NEXTAUTH_SECRET });

  console.log("Token JWT :", token);


    const headers = new Headers({
      'Cache-Control': 'no-store',
    });


    return NextResponse.json(token, headers);
}
