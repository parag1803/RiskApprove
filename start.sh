#!/bin/bash

echo "🚀 Starting RiskApprove..."
echo ""
echo "This will build and start all microservices."
echo "First-time startup may take 10-15 minutes."
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

docker-compose up --build

